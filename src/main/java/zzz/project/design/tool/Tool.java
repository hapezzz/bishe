package zzz.project.design.tool;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Tool {

    public static <T> T map2bean(Map<String, Object> map, Class<T> tClass) {
        if (map == null) {
            return null;
        }
        try {
            T t = tClass.newInstance();
            BeanUtils.populate(t, map);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    public static String getDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(new Date());
    }

    public static String unit2time(long unit) {
        long hours = unit / 3600;
        long min = (unit / 60) % 60;
        long seconds = unit % 60;
        String time = "";
        if (hours == 0) {
            time = min + "分钟," + seconds + "秒";
        } else if (hours > 0) {
            time = hours + "小时," + min + "分钟," + seconds + "秒";
        }
        return time;
    }

    public static boolean isNull(String s) {
        return s == null || s.length() == 0;
    }

    public static HttpServletResponse handleDownloadName(HttpServletRequest request, HttpServletResponse response, String filename) {
        try {
            request.setCharacterEncoding("utf-8");

            String header = request.getHeader("User-Agent");// 获取请求的消息头处理文件名乱码
            header = header.toLowerCase();
            // 设置消息头
            response.addHeader("content-Type", "application/octet-stream");// 设置下载的文件格式为二进制文件
            if (header.contains("firefox")) {// 来自火狐
                response.addHeader("content-Disposition", "attachment;filename==?UTF-8?B?"
                        + new String(Base64.encodeBase64(filename.getBytes("utf-8"))) + "?=");
            } else {// 来自其他浏览器
                response.addHeader("content-Disposition",
                        "attachment;filename=" + URLEncoder.encode(filename, "utf-8"));
            }
            return response;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return response;
        }
    }

    public static HSSFWorkbook initExcel(String[] headers, String filename) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("sheet1");
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell(i);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        return workbook;
    }
}
