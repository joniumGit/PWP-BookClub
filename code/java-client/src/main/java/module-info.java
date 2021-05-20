module pwp.book.club.java.client {
    requires java.logging;
    requires static lombok;
    requires com.fasterxml.jackson.annotation;
    requires dev.jonium.mason;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static jakarta.validation;
    requires java.desktop;
    requires com.miglayout.swing;

    opens pwp.client.model to
            com.fasterxml.jackson.annotation,
            com.fasterxml.jackson.core,
            com.fasterxml.jackson.databind;
    opens pwp.client.model.containers to
            com.fasterxml.jackson.annotation,
            com.fasterxml.jackson.core,
            com.fasterxml.jackson.databind;
}