package uk.gov.pay.publicauth.util;

import org.postgresql.ssl.WrappedFactory;

// You will find no references in the code to this class. It is referenced in the JDBC connection string
public class TrustingSSLSocketFactory  extends WrappedFactory {
    public TrustingSSLSocketFactory() {
        this._factory = TrustStoreLoader.getSSLContext().getSocketFactory();
    }
}
