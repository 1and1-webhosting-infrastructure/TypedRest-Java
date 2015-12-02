package com.oneandone.typedrest;

import java.lang.annotation.*;

/**
 * Marks a property as the entity's ID.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.FIELD})
public @interface Id {
}