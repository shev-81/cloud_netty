package com.cloud.clientpak.handlers;

import com.cloud.clientpak.Controller;
import com.cloud.clientpak.interfaces.RequestHandler;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import lombok.extern.log4j.Log4j2;
import messages.FileMessage;
import messages.FilesSizeRequest;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class listener of messages carrying file data. Defines a method for work
 * with a message containing the file data.
 */
@Log4j2
@Handler(message = "FileMessage")
public class FileHandler implements RequestHandler {

    /**
     * Variable {@link Controller Controller}
     */
    private Controller controller;

    /**
     * Output stream to a file.
     */
    private FileOutputStream fos;

    /**
     * Marker defining the mode of writing to the file
     * (whether to append the data to the end of the file or overwrite it.)
     */
    private boolean append;

    /**
     * The constructor saves a reference to the application controller.
     * @param controller application controller.
     */
    public FileHandler(Controller controller) {
        this();
        this.controller = controller;
        this.fos = null;
    }

    public FileHandler() {
    }

    /**
     * Receives a message with file data, checks whether the file fits into 1 message.
     * If not, then sets the "append" marker to "true" mode to record data from
     * follow-up messages to the end of the file. After receiving the last part of the file in the message
     * closes the output stream to a file, and sends a request to update the file list data
     * @param ctx channel context.
     * @param msg message object.
     */
    @Override
    public void handle(ChannelHandlerContext ctx, Object msg) {
        FileMessage fmsg = (FileMessage) msg;
        try {
            if (fmsg.partNumber == 1) {
                append = false;
                fos = null;
                fos = new FileOutputStream("client/files/" + fmsg.filename, append);
            } else {
                append = true;
            }
            Platform.runLater(() -> {
                controller.setVisibleLoadInfoFile(true);
                controller.changeProgressBar((double) fmsg.partNumber * ((double) 1 / fmsg.partsCount));
            });
            log.info(fmsg.partNumber + " / " + fmsg.partsCount);
            fos.write(fmsg.data);
            if (fmsg.partNumber == fmsg.partsCount) {
                fos.close();
                append = false;
                log.info("файл полностью получен");
                ctx.writeAndFlush(new FilesSizeRequest(1));
                Platform.runLater(() -> {
                    controller.setVisibleLoadInfoFile(false);
                });
            }
        } catch (IOException e) {
            log.error(e.toString());
        }
    }
}
