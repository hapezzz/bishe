package zzz.project.design.tool;

import java.util.Random;

public class VerifyCode {
    private static String codes = "23456789abcdefghjkmnopqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private static String text;                //用来保存验证码的文本内容
    private static Random r = new Random();    //获取随机数对象

    private static char randomChar() {
        int index = r.nextInt(codes.length());
        return codes.charAt(index);
    }

    public static String VerifyCode(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++)
        {
            String s = randomChar() + "";      //随机生成字符，因为只有画字符串的方法，没有画字符的方法，所以需要将字符变成字符串再画
            sb.append(s);                      //添加到StringBuilder里面
        }
        text = sb.toString();
        return text;
    }

}
