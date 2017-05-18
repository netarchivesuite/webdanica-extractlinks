package dk.netarkivet.extractlinks.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.htmlparser.util.ParserException;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

/**
 * 
 * Extract links from html records in arc and warc response records.
 *
 */
public class ExtractLinksBatchJob extends ArchiveBatchJob {
    
    private long collectedLinks;
    private long skippedRecords;
    boolean onlyexamineDKUrlRecords;
    boolean onlyfindurlsoutsidedotdk;

    public ExtractLinksBatchJob() {
    	this.onlyexamineDKUrlRecords = false;
    	this.onlyfindurlsoutsidedotdk = false;
    }

    public ExtractLinksBatchJob(boolean onlyexamineDKUrlRecords, boolean onlyfindurlsoutsidedotdk) {
    	this.onlyexamineDKUrlRecords = onlyexamineDKUrlRecords;
    	this.onlyfindurlsoutsidedotdk = onlyfindurlsoutsidedotdk;
    }

    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) {
        ArchiveHeaderBase metadata = record.getHeader();
        if (metadata.bIsArc) {
        	processArc(record, metadata, os);
        } else if (metadata.bIsWarc) {
        	processWarc(record, metadata, os);
        }
    }
    
    private void processWarc(ArchiveRecordBase record,
    		ArchiveHeaderBase header, OutputStream os) {

    	String msgType;
    	String mimeType = header.getMimetype();
    	ContentType contentType = ContentType.parseContentType(mimeType);
    	boolean bResponse = false;
    	if (contentType != null) {
    		if ("application".equals(contentType.contentType) && "http".equals(contentType.mediaType)) {
    			msgType = contentType.getParameter("msgtype");
    			if ("response".equals(msgType)) {
    				bResponse = true;
    			} else if ("request".equals(msgType)) {
    			}
    		}
    		mimeType = contentType.toStringShort();
    	}
    	ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(record.getInputStream(), 8192);
    	HttpHeader httpResponse = null;
    	if (bResponse) {
    		try {
    			httpResponse = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, pbin, header.getLength(), null);
    			if (httpResponse != null && httpResponse.contentType != null) {
    				contentType = ContentType.parseContentType(httpResponse.contentType);
    				if (contentType != null) {
    					mimeType = contentType.toStringShort();
    				}
    			}
    		} catch (IOException e) {
    			throw new IOFailure("Error reading WARC httpresponse header", e);
    		}
    		
    		if (mimeType.toUpperCase().contains("HTML")) {
    			getLinks(record, os, this.onlyfindurlsoutsidedotdk);
    		} else {
    			skippedRecords++;
    		}
    	} 
    }

	private void getLinks(ArchiveRecordBase record, OutputStream os, boolean onlyFindUrlsOutsideDK ) {
        String text = iostreamToString(record.getInputStream());

        ExtractLinks extractor = new ExtractLinks(text);
        try {
            for (String link: extractor.getOutLinks(onlyFindUrlsOutsideDK)) {
                os.write(new String("\n" + link).getBytes("UTF-8"));
                collectedLinks++;
            }
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } 
	}

	private void processArc(ArchiveRecordBase record,
			ArchiveHeaderBase metadata, OutputStream os) {
		// Skip, if not html and not .dk URL
		if (!metadata.getMimetype().toUpperCase().contains("HTML")){  
			skippedRecords++;
		} else {
			try {
				if (onlyexamineDKUrlRecords && !ExtractLinks.urlIsDK(metadata)) {
					skippedRecords++;
				}else {
					getLinks(record, os, this.onlyfindurlsoutsidedotdk);
				}
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
		}

	}

	@Override
    public void initialize(OutputStream os) {
        skippedRecords = 0L;
        collectedLinks = 0L;
        
    }

    @Override
    public void finish(OutputStream os) {
        String res = "\nFound " + collectedLinks + " links which matches the "
                + "not DK-criteria";
        res += "\nSkipped " + skippedRecords + " records";
        
        try {
            os.write(res.getBytes());
        } catch (IOException e) {
            new IOFailure("Could not write string: " + res, e);
        }    
    }
    
    /**
     * Convert inputstream into text string. Wrapper method around IOUtils.toString method tyo catch any exceptions.
     * @param in the given inputstream
     * @return a textstring or null if something went wrong
     */
    private static String iostreamToString(InputStream in) {
        String res = null;
        try {
            res = IOUtils.toString(in, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
       
        return res;
    }
}
