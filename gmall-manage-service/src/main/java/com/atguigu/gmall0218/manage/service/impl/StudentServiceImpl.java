package com.atguigu.gmall0218.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0218.bean.Student;
import com.atguigu.gmall0218.service.StudentService;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {
    @Override
    public List<Student> findAll() {
        return null;
    }
}
