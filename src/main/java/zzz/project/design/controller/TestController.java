package zzz.project.design.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import zzz.project.design.tool.Email;
import zzz.project.design.tool.ImageVerificationCode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;

@Controller
public class TestController {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;
    @RequestMapping("/test")
    @ResponseBody
    public void test(){
        Email.sendEmail("1448787060@qq.com","测试","这是测试信息，789432",javaMailSender,from);
    }


}
