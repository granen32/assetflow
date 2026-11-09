package com.assetflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 이 클래스의 패키지 아래에 있는 Controller·Service·Repository 등을 Spring이 발견한다.
// 그래서 기능 패키지를 com.assetflow 아래에 두면 별도 스캔 경로 설정이 필요 없다.
@SpringBootApplication
public class AssetFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetFlowApplication.class, args);
    }
}
