package com.example.chat.prompt;

public class UnknownTemplateException extends RuntimeException {
    public UnknownTemplateException() { super("Unknown template identifier"); }
}
