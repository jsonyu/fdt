
package lia.util.net.copy.transport;

import java.io.Serializable;
import java.util.Comparator;

import lia.util.net.copy.transport.internal.FDTSelectionKey;


public class FDTWriterKeyAttachementComparator implements Comparator<FDTSelectionKey>, Serializable {
    
    
    private static final long serialVersionUID = -9190255291921632210L;

    public int compare(final FDTSelectionKey sk1, final FDTSelectionKey sk2) {
        
        if(sk1 == sk2) return 0;
        
        if(sk1 == FDTSelectionKey.END_PROCESSING_NOTIF_KEY) return 1;
        if(sk2 == FDTSelectionKey.END_PROCESSING_NOTIF_KEY) return -1;
        
        final FDTWriterKeyAttachement sk1Attach = (FDTWriterKeyAttachement)sk1.attachment();
        final FDTWriterKeyAttachement sk2Attach = (FDTWriterKeyAttachement)sk2.attachment();
        
        return sk1Attach.compareTo(sk2Attach);
    }
    
}