package pwp.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pwp.client.utils.Immutable;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true, of = {"user"})
@NoArgsConstructor
public class UserBook extends Book {

    public enum ReadingStatus {
        @JsonProperty("pending") PENDING,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("reading") READING
    }

    @Immutable
    private String user;
    @JsonProperty("reading_status")
    private ReadingStatus status;
    private Boolean reviewed;
    private Boolean ignored;
    private Boolean liked;
    @JsonProperty("current_page")
    private Integer atPage;
}
