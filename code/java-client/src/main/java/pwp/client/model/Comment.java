package pwp.client.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import pwp.client.utils.ID;
import pwp.client.utils.Immutable;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"uuid"})
@NoArgsConstructor
public class Comment {
    @ID
    @Immutable
    private String uuid;
    private String user;
    @NotNull
    private String content;
}
