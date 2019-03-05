package com.swpu.uchain.openexperiment.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swpu.uchain.openexperiment.enums.CodeMsg;
import lombok.Data;

/**
 * 结果返回类封装
 * @author clf
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    private Result(T data) {
        this.code=0;
        this.msg="success";
        this.data = data;
    }

    private Result(CodeMsg codeMsg) {
        if (codeMsg==null) {
            return;
        }
        this.code = codeMsg.getCode();
        this.msg = codeMsg.getMsg();
    }

    public static <T> Result<T> success(T data){
        return new Result<T>(data);
    }

    public static Result success(){
        return success(null);
    }

    public static <T> Result<T> error(CodeMsg codeMsg){
        return new Result<T>(codeMsg);
    }
}
