package io.fair_acc.sample.financial.service.execution;

import io.fair_acc.sample.financial.dos.Order;

public class ExecutionResult {
    public enum ExecutionResultEnum {
        OK,
        ERROR,
        CANCEL
    }

    private final Order order;
    private ExecutionResultEnum result;
    private String errorMessage;

    public ExecutionResult(Order order) {
        this(ExecutionResultEnum.OK, order);
    }

    public ExecutionResult(ExecutionResultEnum resultEnum, Order order) {
        this.order = order;
        setResult(resultEnum);
    }

    public Order getOrder() {
        return order;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ExecutionResultEnum getResult() {
        return result;
    }

    public void setResult(ExecutionResultEnum result) {
        this.result = result;
    }
}
