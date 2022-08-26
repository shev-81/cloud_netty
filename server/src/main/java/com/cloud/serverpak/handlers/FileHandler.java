package com.cloud.serverpak.handlers;

import com.cloud.serverpak.services.FilesInformService;
import com.cloud.serverpak.MainHandler;
import io.netty.channel.ChannelHandlerContext;
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
public class FileHandler {

    /**
     * Netty's main listener.
     * @see MainHandler
     */
    private final MainHandler mainHandler;

    /**
     * File information service.
     * @see FilesInformService
     */
    private final FilesInformService fileService;

    /**
     * Output stream to a file.
     */
    private FileOutputStream fos;

    /**
     * Marker defining the mode of writing to the file
     * (whether to append the data to the end of the
     * file or overwrite it.)
     * */
    private boolean append;

    /**
     * The constructor saves the main listener reference.
     * @param mainHandler Netty's main listener.
     */
    public FileHandler(MainHandler mainHandler) {
        this.mainHandler = mainHandler;
        this.fileService = mainHandler.getFilesInformService();
    }

    /**
     * Receives a message with file data, checks whether the file fits into 1 message.
     * If not, then sets the "append" marker to "true" mode to record data from
     * follow-up messages to the end of the file. After receiving the last part of the file in the message
     * closes the output stream to the file. Sends a service message to the client with updated
     * storage status data.
     * @see FilesSizeRequest
     * @param ctx channel context.
     * @param msg the message object.
     */
    public void fileHandle(ChannelHandlerContext ctx, Object msg) {
        String userName = mainHandler.getUserName();
        FileMessage fmsg = (FileMessage) msg;
        try {
            if (fmsg.partNumber == 1) {
                append = false;
                fos = null;
                fos = new FileOutputStream("server/files/" + userName + "/" + fmsg.filename, append);
            } else {
                append = true;
            }
            log.info(fmsg.partNumber + " / " + fmsg.partsCount);
            fos.write(fmsg.data);
            if (fmsg.partNumber == fmsg.partsCount) {
                fos.close();
                append = false;
                log.info("Файл полностью получен");
            }
            ctx.writeAndFlush(new FilesSizeRequest(
                    fileService.getFilesSize(userName),
                    fileService.getListFiles(userName),
                    fmsg.partNumber,
                    fmsg.partsCount)
            );
        } catch (IOException e) {
            log.error(e.toString());
        }
    }
}
