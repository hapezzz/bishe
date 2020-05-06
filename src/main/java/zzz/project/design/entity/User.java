package zzz.project.design.entity;

import lombok.Data;

@Data
public class User {
    private String user_name;
    private String user_sex;
    private String email;//唯一
    private String birthday;
    private int user_age;
    private int manager;//0 1 2

    private String account;//唯一，8-16位
    private String password;//md5加密
}
