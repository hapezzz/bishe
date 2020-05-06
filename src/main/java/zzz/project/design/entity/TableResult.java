package zzz.project.design.entity;

import lombok.Data;

import java.util.LinkedHashSet;

@Data
public class TableResult {
    private String code ="0";
    private long count;
    private LinkedHashSet<User> data;
}
