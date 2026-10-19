INSERT INTO users (username ,password,phone,email,role,status)
SELECT 'admin' ,'$2a$10$qn.pn2xR48ImUHYKh47LTOyn/pAIXIGipzn8sTRCPnTVntgDBilai',NULL,NULL,0,1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username= 'admin');