package zzz.project.design.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import zzz.project.design.entity.Count;
import zzz.project.design.entity.Message;
import zzz.project.design.service.FunctionService;
import zzz.project.design.tool.ImageVerificationCode;
import zzz.project.design.tool.Tool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Controller
public class FunctionController {

    @Autowired
    private FunctionService functionService;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @RequestMapping("/getVerifyCode")
    @ResponseBody
    public void VerifyCode(HttpSession session,HttpServletRequest request, HttpServletResponse response){
        ImageVerificationCode ivc = new ImageVerificationCode();     //用我们的验证码类，生成验证码类对象
        BufferedImage image = ivc.getImage();  //获取验证码
        request.getSession().setAttribute("text", ivc.getText()); //将验证码的文本存在session中
        try{
            session.setAttribute("verify",ivc.getText());
            ivc.output(image, response.getOutputStream());//将验证码图片响应给客户端
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequestMapping("/login")
    @ResponseBody
    public int login(@RequestBody Map<String,String> map,HttpSession session){
        String verify = (String)session.getAttribute("verify");
        String code = map.get("verify");
        if(verify.equalsIgnoreCase(code)){
           String password = DigestUtils.md5DigestAsHex(map.get("password").getBytes());
           String account = map.get("account");
           session.setAttribute("account",account);
           redisTemplate.opsForHash().put("numoflogin",account,account);
           return functionService.login(account,password);
        }else{
            return -3;//验证码不正确
        }
    }

    @RequestMapping("/excel")
    @ResponseBody
    public void excel(HttpServletRequest request,HttpServletResponse response){
        functionService.excelInfo(response, request);
    }

    @RequestMapping("/message")
    @ResponseBody
    public boolean message(HttpSession session,HttpServletRequest request){
        String account = (String) session.getAttribute("account");
        if(!Tool.isNull(account)){
            String content = request.getParameter("content");
            log.debug(content);
            if(Tool.isNull(content)){
                return false;
            }
            return functionService.message(content,account);
        }else {
            return false;
        }
    }

    @RequestMapping("/viewMessages")
    @ResponseBody
    public LinkedList<Message> viewMessages(){
        return functionService.viewMessages();
    }

    @PostMapping("/resetPassword")
    @ResponseBody
    public int resetPassword(@RequestBody Map<String,Object> map,HttpSession session){
        String account = (String) session.getAttribute("account");
        if(map==null||Tool.isNull(account)){
            return -1;
        }
        return functionService.resetPassword(account,map);
    }

    @RequestMapping("/logout")
    @ResponseBody
    public int logout(HttpSession session){
        try{
            session.removeAttribute("account");
            return 1;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }
    @RequestMapping("/grant")
    @ResponseBody
    public int grant(HttpServletRequest request){
        String account = request.getParameter("account");
        System.out.println(account);
        return functionService.grant(account);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public int remove(HttpServletRequest request){
        String account = request.getParameter("account");
        return functionService.remove(account);
    }

    @RequestMapping("/getData")
    @ResponseBody
    public ArrayList<Count> getData(){
        Map<Object,Object> countofadd = redisTemplate.opsForHash().entries("countofadd");
        Map<Object,Object> countoflogin = redisTemplate.opsForHash().entries("countoflogin");
        ArrayList<Count> counts = new ArrayList<>();
        Iterator<Map.Entry<Object,Object>> add_iterator = countofadd.entrySet().iterator();
        Iterator<Map.Entry<Object,Object>> login_iterator = countoflogin.entrySet().iterator();
        while (add_iterator.hasNext()){
            Count count = new Count();
            Map.Entry<Object,Object> add_entry = add_iterator.next();
            Map.Entry<Object,Object> login_entry = login_iterator.next();
            String date = JSONObject.parseObject(JSONObject.toJSONString(add_entry.getKey()),String.class);
            Long add = JSONObject.parseObject(JSONObject.toJSONString(add_entry.getValue()),Long.class);
            Long login = JSONObject.parseObject(JSONObject.toJSONString(login_entry.getValue()),Long.class);
            count.setDate(date);
            count.setAdd(add);
            count.setLogin(login);
            counts.add(count);
        }
        return counts;
    }
    @RequestMapping("/getDate")
    @ResponseBody
    public ArrayList<String> getDate(){
        Map<Object,Object> countofadd = redisTemplate.opsForHash().entries("countofadd");
        ArrayList<String> date = new ArrayList<>();
        Iterator<Object> iterator = countofadd.keySet().iterator();
        while(iterator.hasNext()){
            date.add(JSONObject.parseObject(JSONObject.toJSONString(iterator.next()),String.class));
        }
        return date;
    }
    @RequestMapping("/findPwd")
    @ResponseBody
    public int findPwd(HttpServletRequest request){
        String email = request.getParameter("email");
        System.out.println(email);
        return functionService.findPwd(email);
    }
    @RequestMapping("/resetpwd")
    @ResponseBody
    public int resetpwd(HttpServletRequest request){
        String verify = request.getParameter("verify");
        String newPwd = request.getParameter("newPwd");
        String email = request.getParameter("email");
        return functionService.resetpwd(verify,newPwd,email);
    }
}
