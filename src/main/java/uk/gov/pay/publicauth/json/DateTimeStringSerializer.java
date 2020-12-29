package uk.gov.pay.publicauth.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeStringSerializer extends StdSerializer<ZonedDateTime> {

    public DateTimeStringSerializer() {
        this(null);
    }

    private DateTimeStringSerializer(Class<ZonedDateTime> t) {
        super(t);
    }
    
    @Override
    public void serialize(ZonedDateTime zonedDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm").format(zonedDateTime));
    }
}
