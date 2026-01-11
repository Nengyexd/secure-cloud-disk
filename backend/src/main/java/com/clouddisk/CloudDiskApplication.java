package com.clouddisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.clouddisk.mapper")
@EnableAsync
@EnableScheduling
public class CloudDiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDiskApplication.class, args);
        System.out.println("\n==============================================");
        System.out.println("  吉亦云盘系统启动成功！");
        System.out.println("  访问地址: http://localhost:3000/api");
        System.out.println("==============================================\n");
    }
}
    