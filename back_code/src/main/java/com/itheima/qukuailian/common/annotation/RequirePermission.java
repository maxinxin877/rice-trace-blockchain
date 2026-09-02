package com.itheima.qukuailian.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解：标注在 Controller 方法或类上，要求当前用户具备指定权限码。
 * <p>示例：{@code @RequirePermission("RICE_FIELD_CREATE")}</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限码，见 {@code PermissionConstants} */
    String value();
}
