package zzz.project.design.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import zzz.project.design.dao.UserDao;
import zzz.project.design.entity.TableResult;
import zzz.project.design.entity.User;
import zzz.project.design.service.UserService;
import zzz.project.design.tool.Tool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class UserController {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @PostMapping("/addTempUser")
    @ResponseBody
    public int addTempUser(@RequestBody Map<String,Object> map){
        String password = (String)map.get("password");
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        map.put("password",password);
        return userService.addTempUser(map);
    }

    @RequestMapping("/resendVU")
    @ResponseBody
    public int resendVU(HttpServletRequest request,HttpSession session){
        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String account = (String)session.getAttribute("account");
        if(!Tool.isNull(account)){
            User user = userDao.getTempUser(account);
            email = user.getEmail();
            name = user.getUser_name();
        }
        return userService.sendVUserC(email,name);
    }

    @RequestMapping("/VerifyUser")
    @ResponseBody
    public int VerifyUser(@RequestBody Map<String,Object> map,HttpSession session){
        String email = (String)map.get("email");
        String account = (String) session.getAttribute("account");
        if(!Tool.isNull(account)){
            email = userDao.getTempUser(account).getEmail();
        }
        String verify = (String)map.get("verify");
        if(redisTemplate.hasKey(email+"-verify")){
            String code = JSONObject.parseObject(JSONObject.toJSONString(redisTemplate.opsForValue().get(email+"-verify")),String.class);
            if(code.equals(verify)){
                return userService.addUser(email);
            }else{
                return -1;
            }
        }else {
            return -2;
        }
    }

    @RequestMapping("/viewUsers")
    @ResponseBody
    public TableResult viewUsers(HttpServletRequest request){
        //temp  key
        String query_key = request.getParameter("query_key");//查询关键字，针对正式用户
        String temp = request.getParameter("temp");//是否是正式用户

        logger.debug(temp+"-"+query_key);

        long page = Long.parseLong(request.getParameter("page")) ;
        long limit = Long.parseLong(request.getParameter("limit")) ;
        long offset = (page-1)*limit;
        return userService.viewUsers(query_key,temp,limit,offset);
    }

    @RequestMapping("/getCurUser")
    @ResponseBody
    public User getCurUser (HttpSession session){
        String account = (String) session.getAttribute("account");
        if(Tool.isNull(account)){
            return null;
        }else {
            return userDao.getUser(account);
        }
    }

    @PostMapping("/resetProp")
    @ResponseBody
    public int resetProp(@RequestBody Map<Integer,Object> map, HttpSession session){
        String account = (String)session.getAttribute("account");
        if(Tool.isNull(account)){
            return -1;
        }
        return userService.resetProp(account,map);
    }

    @RequestMapping("/deleteUser")
    @ResponseBody
    public int deleteUser(HttpSession session){
        String account = (String)session.getAttribute("account");
        if(Tool.isNull(account)){
            return -1;
        }else{
            if(userDao.deleteUser(account)){
                return 1;
            }else {
                return 0;
            }
        }
    }
}
