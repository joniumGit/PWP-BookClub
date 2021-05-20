package pwp.client.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import pwp.client.utils.ID;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"handle"})
@NoArgsConstructor
public class Club {
    @ID
    @NotNull
    private String handle;
    private String owner;
    private String description;
}
