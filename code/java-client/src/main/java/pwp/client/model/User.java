package pwp.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pwp.client.utils.ID;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"handle"})
@NoArgsConstructor
public class User {
    @ID
    @JsonProperty("username")
    @NotNull
    private String handle;
    private String description;
}
