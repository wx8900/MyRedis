package model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserDO implements Serializable {

    private Integer id;
    private String name;
    private Integer age;
    private Integer sex;
    private String unick;
    private String email;

    @Synchronized
    public static void main(String[] args) {
        UserDO test = UserDO.builder().id(1231).age(22).name("test2").build();
        System.out.println(test.toString());
        log.info("log ------------testing! "+ test.getName());
    }

}