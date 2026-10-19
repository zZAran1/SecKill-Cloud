package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.constant.UserConstants;
import com.example.converter.UserConverter;
import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.model.dto.ChangePasswordDTO;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.dto.UpdateProfileDTO;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.result.ErrorCode;
import com.example.result.PageResult;
import com.example.security.LoginFailLimiter;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,Users> implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /**与网关 JwtGatewayFilter 约定的账号封禁黑名单 key*/
    private static final String USER_BLACKLIST_KEY = "user:blacklist:";

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final LoginFailLimiter limiter;

    private final StringRedisTemplate redisTemplate;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           LoginFailLimiter limiter,
                           StringRedisTemplate redisTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.limiter = limiter;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto){
        Users users= UserConverter.INSTANCE.toEntity(dto);
        users.setPassword(passwordEncoder.encode(dto.getPassword()));
        users.setStatus(UserConstants.STATUS_ENABLED);
        users.setRole(UserConstants.ROLE_USER);
        try{
            this.save(users);
        }catch (DuplicateKeyException e){
            throw new UserException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    @Override
    public TokenVO login(LoginDTO dto){
        limiter.checkNotLocked(dto.getPhone());
        Users dbUser=this.lambdaQuery()
                .eq(Users::getPhone,dto.getPhone())
                .one();
        if(dbUser==null || !passwordEncoder.matches(dto.getPassword(),dbUser.getPassword())){
            limiter.onFail(dto.getPhone());
            throw new UserException(ErrorCode.LOGIN_FAILED);
        }
        if(dbUser.getStatus()!= UserConstants.STATUS_ENABLED){
            throw new UserException(ErrorCode.ACCOUNT_DISABLED);
        }
        TokenVO tokenVO=new TokenVO();
        tokenVO.setToken(jwtUtil.generateToken(dbUser.getId(),dbUser.getUsername(),String.valueOf(dbUser.getRole()),String.valueOf(dbUser.getStatus())));
        limiter.onSuccess(dto.getPhone());
        return tokenVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId){
        Users user=getById(userId);
        if(user==null){
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        return UserConverter.INSTANCE.toVO(user);
    }

    @Override
    public void logout(){

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateProfile(Long userId, UpdateProfileDTO dto) {
        Users user = getById(userId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        try {
            this.update(new LambdaUpdateWrapper<Users>()
                    .eq(Users::getId, userId)
                    .set(StringUtils.hasText(dto.getUsername()), Users::getUsername, dto.getUsername())
                    .set(StringUtils.hasText(dto.getEmail()), Users::getEmail, dto.getEmail())
                    .set(StringUtils.hasText(dto.getSex()), Users::getSex, dto.getSex())
                    .set(dto.getAge() != null, Users::getAge, dto.getAge())
                    .set(StringUtils.hasText(dto.getAddress()), Users::getAddress, dto.getAddress()));
        } catch (DuplicateKeyException e) {
            throw new UserException(ErrorCode.USER_ALREADY_EXISTS);
        }
        return UserConverter.INSTANCE.toVO(getById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        Users user = getById(userId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_PASSWORD_WRONG);
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_PASSWORD_WRONG, "新密码不能与原密码相同");
        }
        this.update(new LambdaUpdateWrapper<Users>()
                .eq(Users::getId, userId)
                .set(Users::getPassword, passwordEncoder.encode(dto.getNewPassword())));
    }

    @Override
    public PageResult<UserInfoVO> pageUsers(String keyword, long page, long size) {
        Page<Users> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Users>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Users::getUsername, keyword)
                                .or().like(Users::getPhone, keyword)
                                .or().like(Users::getEmail, keyword))
                        .orderByDesc(Users::getCreateTime));
        return PageResult.of(
                result.getRecords().stream().map(UserConverter.INSTANCE::toVO).toList(),
                result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null
                || (status != UserConstants.STATUS_ENABLED && status != UserConstants.STATUS_DISABLED)) {
            throw new UserException(ErrorCode.BAD_REQUEST, "状态值不合法，只支持 0 封禁 / 1 正常");
        }
        Users user = getById(userId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        this.update(new LambdaUpdateWrapper<Users>()
                .eq(Users::getId, userId)
                .set(Users::getStatus, status));

        // 封禁：写入黑名单时间戳，网关拒掉该时间点之前签发的所有 token
        // 解封：删除黑名单，之后签发的 token 立即恢复访问
        String key = USER_BLACKLIST_KEY + userId;
        if (status == UserConstants.STATUS_DISABLED) {
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
        } else {
            redisTemplate.delete(key);
        }
        log.info("账号状态变更 userId={}, status={}", userId, status);
    }
}
