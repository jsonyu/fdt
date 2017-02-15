
package lia.util.net.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.UUID;


public class FileWriterSession extends FileSession {

    private volatile boolean channelInitialized;
    
    private File tmpCopyFile;
    private String openMode = "rw";
    
    public FileWriterSession(UUID uid, String fileName, long size, long lastModified, boolean isLoop, String writeMode) throws IOException {
        super(uid, fileName, isLoop);
        
        if(!isNull) {
            this.sessionSize = size;
            this.lastModified = lastModified;
            
            String tmpF = "";
            if(!file.exists()) {
                
                File dirs = new File(file.getParent());
                if(!dirs.exists()) {
                    if( !dirs.mkdirs() ) {
                        throw new IOException(" Unable to create parent dirs [ " + dirs + " ]");
                    }
                }

            }

            File parent = file.getParentFile();
            if(parent != null) {
                tmpF = parent.getAbsolutePath();
            }
            
            this.tmpCopyFile = new File(tmpF + File.separator + "." + file.getName());
            
        } else {
            this.sessionSize = size;
            this.tmpCopyFile = file;
        }

        if(this.tmpCopyFile.exists()) {
            partitionID = PartitionMap.getPartitionFromCache(this.tmpCopyFile.getAbsolutePath());
        } else {
            partitionID = PartitionMap.getPartitionFromCache(this.tmpCopyFile.getParent());
        }

        channelInitialized = false;
        if(writeMode == null || writeMode.equalsIgnoreCase("nosync")) {
            openMode = "rw";
        } else if(writeMode.equalsIgnoreCase("dsync")) {
            openMode = "rwd";
        } else if(writeMode.equalsIgnoreCase("sync")) {
            openMode = "rws";
        } else {
            openMode = "rw";
        }
          
    }

    public FileChannel getChannel() throws Exception {

        if(channelInitialized) {
            if(isClosed()) {
                if(!isLoop) {
                    throw new Exception("Stream closed!");
                }
            } else {
                return fileChannel;
            }
        }
        
        synchronized(closeLock) {
            
            if(channelInitialized) {
                if(isClosed()) throw new Exception("Stream closed!");
                return fileChannel;
            }
            
            if(isClosed()) throw new Exception("Stream closed!");
            try {
                if(shouldFlush) {
                    fileChannel = new RandomAccessFile(tmpCopyFile, openMode).getChannel();
                } else {
                    fileChannel = new FileOutputStream(tmpCopyFile).getChannel();
                }
                
                channelInitialized = true;
            }catch(Exception ex) {
                close(null, ex);
                throw ex;
            }
        }
        
        return fileChannel;
    }
    
    protected void internalClose() {
        super.internalClose();

        if(!isNull && file != null && tmpCopyFile != null && downCause() == null) {
            tmpCopyFile.renameTo(file);
            file.setLastModified(lastModified);
        } else {
            if(downCause() != null || downMessage() != null && tmpCopyFile != null) {
                tmpCopyFile.delete();
            }
        }

        if(isLoop) {
            
            channelInitialized = false;
            closed = false;
        }
    }

}
