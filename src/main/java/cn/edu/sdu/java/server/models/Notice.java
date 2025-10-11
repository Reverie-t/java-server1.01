package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/*
 * User用户表实体类 保存每个允许登录的信息人员的账号信息，
 * Integer personId 用户表 user 主键 person_id
 * UserType userType 关联到用户类型对象
 * Person person 关联到该用户所用的Person对象，账户所对应的人员信息 person_id 关联 person 表主键 person_id
 * String userName 登录账号 和Person 中的num属性相同
 * String password 用户密码 非对称加密，这能加密，无法解码
 *
 */
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Text;

@Getter
@Setter
@Entity
@Table(	name = "notice",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
public class Notice {
    @Id
    private Integer id;
    @Size(max=30)
    private String title;
    @Size
    private String context;
    @Size(max = 20)
    private String  time;

}
