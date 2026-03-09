package com.serain.serainaicode;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.serain.serainaicode.mapper")
public class SerainAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SerainAiCodeApplication.class, args);
    }

}
