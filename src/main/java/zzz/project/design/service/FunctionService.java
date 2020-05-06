package zzz.project.design.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import zzz.project.design.dao.UserDao;
import zzz.project.design.entity.Message;
import zzz.project.design.entity.User;
import zzz.project.design.tool.Email;
import zzz.project.design.tool.Tool;
import zzz.project.design.tool.VerifyCode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FunctionService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private UserDao userDao;
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;

    public int login(String account,String password){
        if(redisTemplate.opsForHash().hasKey("accounts",account)){
            double score = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForHash().get("accounts",account)),Double.class);
            Set<Object> set = redisTemplate.opsForZSet().rangeByScore("users",score,score);
            Set<Object> tempset = redisTemplate.opsForZSet().rangeByScore("tempUsers",score,score);
            if(tempset.size()>0){
                return -1;//未通过邮箱验证
            }else{
                User user = null;
                for(Object temp:set){
                    user = JSONObject.parseObject(JSONObject.toJSONString(temp),User.class);
                }
                if(user.getPassword().equals(password)){
                    return user.getManager();
                }else{
                    return -4;
                }
            }
        }else{
            return -2;//无此人
        }
    }

    public void excelInfo(HttpServletResponse response, HttpServletRequest request){
        String filename = "用户信息表.xls";
        String  [] headers = {"帐号","用户名","年龄","性别","邮箱","出生日期","身份"};
        response = Tool.handleDownloadName(request,response,filename);
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        HSSFWorkbook wb = Tool.initExcel(headers,filename);
        long max = redisTemplate.opsForZSet().zCard("users");
        Set<Object> set = redisTemplate.opsForZSet().range("users",0,--max);
        HSSFSheet sheet = wb.getSheet("sheet1");
        int rowNum = 1;
        for(Object temp:set){
            User user = JSONObject.parseObject(JSONObject.toJSONString(temp),User.class);
            HSSFRow row1 = sheet.createRow(rowNum);
            row1.createCell(0).setCellValue(user.getAccount());
            row1.createCell(1).setCellValue(user.getUser_name());
            row1.createCell(2).setCellValue(user.getUser_age());
            row1.createCell(3).setCellValue(user.getUser_sex());
            row1.createCell(4).setCellValue(user.getEmail());
            row1.createCell(5).setCellValue(user.getBirthday());
            row1.createCell(6).setCellValue(user.getManager()==0?"普通用户":"管理员");
            rowNum++;
        }
        try{
            wb.write(response.getOutputStream());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean message(String content,String account){
        Message message = new Message();
        message.setAccount(account);
        message.setContent(content);
        message.setUser_name(userDao.getName(account));
        String key = account+":"+Tool.getDate()+" "+Tool.getTime();
        message.setDate(Tool.getDate()+" "+Tool.getTime());
        try{
            redisTemplate.opsForHash().put("messages",key,message);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public LinkedList<Message> viewMessages(){
        Map<Object,Object> map = redisTemplate.opsForHash().entries("messages");
        LinkedList<Message> messages = new LinkedList<>();
        Set<Map.Entry<Object,Object>> set = map.entrySet();
        for (Map.Entry<Object, Object> entry : set) {
//                String key = JSONObject.parseObject(JSONObject.toJSONString(entry.getKey()), String.class);
            Message message = JSONObject.parseObject(JSONObject.toJSONString(entry.getValue()), Message.class);
            messages.add(message);
        }
        messages.sort(new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o2.getDate().compareToIgnoreCase(o1.getDate());
            }
        });
        return messages;
    }

    public int resetPassword(String account, Map<String, Object> map) {
        String old_pwd = (String) map.get("old_pwd");
        old_pwd = DigestUtils.md5DigestAsHex(old_pwd.getBytes());
        User user = userDao.getUser(account);
        double score = redisTemplate.opsForZSet().score("users",user);
        String ver_pwd = user.getPassword();
        if(old_pwd.equals(ver_pwd)){
            String new_pwd = (String)map.get("new_pwd");
            user.setPassword(DigestUtils.md5DigestAsHex(new_pwd.getBytes()));
            if(userDao.updateUser(user,score)){
                return 1;
            }else{
                return -1;
            }

        }else{
            return 0;
        }
    }

    public int grant(String account) {
        User user = userDao.getUser(account);
        if(user==null){
            return -1;
        }
        double score = redisTemplate.opsForZSet().score("users",user);
        user.setManager(1);
        if(userDao.updateUser(user,score)){
            return 1;
        }else{
            return 0;
        }
    }

    public int remove(String account) {
        User user = userDao.getUser(account);
        if(user==null){
            return -1;
        }
        double score = redisTemplate.opsForZSet().score("users",user);
        user.setManager(0);
        if(userDao.updateUser(user,score)){
            return 1;
        }else{
            return 0;
        }
    }

    public int findPwd(String email) {
        User user = userDao.EgetUser(email);
        if(user==null){
            return -2;
        }
        String verify = VerifyCode.VerifyCode();
        String to = email;

        int total = 15;
        long timeout = 180;
        long left = 24 * 3600;
        if (redisTemplate.hasKey(to + "-Ftotal")) {
            left = redisTemplate.getExpire(to + "-Ftotal", TimeUnit.SECONDS);
            total = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForValue().get(to + "-Ftotal")), Integer.class);
            if (total <= 0) {
                return -1;
            }
            total --;
            if (left < 180) {
                timeout = left;
            }
        }
        redisTemplate.opsForValue().set(to + "-Ftotal", total);
        redisTemplate.opsForValue().set(to + "-Fverify", verify);
        redisTemplate.expire(to + "-Ftotal", left, TimeUnit.SECONDS);
        redisTemplate.expire(to + "-Fverify", timeout, TimeUnit.SECONDS);

        String subject = "验证用户邮箱";
        String content = "<html lang='en'><head><meta charset='utf-8'></meta></head><body>用户"+user.getUser_name()+"：您此次的验证码是：<br>"+verify+"<br>输入时注意大小写，此验证码有效期为"+Tool.unit2time(timeout)+"，在接下来"+Tool.unit2time(left)+"还可以发送"+total+"次</body></html>";
        Email.sendEmail("1448787060@qq.com", subject, content, javaMailSender, from);
        return 1;
    }

    public int resetpwd(String verify, String newPwd,String email) {
        User user = userDao.EgetUser(email);
        System.out.println(email);
        if(!redisTemplate.hasKey(email+"-Fverify")){
            return -1;
        }
        String code = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForValue().get(email+"-Fverify")),String.class);
        if(code.equals(verify)){
            user.setPassword(DigestUtils.md5DigestAsHex(newPwd.getBytes()));
            return 1;
        }else{
            return 0;
        }
    }
}
