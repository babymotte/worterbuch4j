
package net.bbmsoft.worterbuch.client.model;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * A message sent by the server to indicate a successful handshake with the client
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "protocolVersion"
})
@Generated("jsonschema2pojo")
public class Handshake {

    /**
     * A protocol version with a major and minor version tag
     * (Required)
     * 
     */
    @JsonProperty("protocolVersion")
    @JsonPropertyDescription("A protocol version with a major and minor version tag")
    private ProtocolVersion protocolVersion;

    /**
     * A protocol version with a major and minor version tag
     * (Required)
     * 
     */
    @JsonProperty("protocolVersion")
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * A protocol version with a major and minor version tag
     * (Required)
     * 
     */
    @JsonProperty("protocolVersion")
    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Handshake.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("protocolVersion");
        sb.append('=');
        sb.append(((this.protocolVersion == null)?"<null>":this.protocolVersion));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.protocolVersion == null)? 0 :this.protocolVersion.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Handshake) == false) {
            return false;
        }
        Handshake rhs = ((Handshake) other);
        return ((this.protocolVersion == rhs.protocolVersion)||((this.protocolVersion!= null)&&this.protocolVersion.equals(rhs.protocolVersion)));
    }

}
