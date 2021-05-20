package pwp.client.model.containers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import dev.jonium.mason.MasonControl;
import dev.jonium.mason.features.MasonControlsFeature;
import dev.jonium.mason.impl.SimpleMason;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Container<T> implements MasonControlsFeature {
    @Setter(onMethod_ = {@JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)})
    private List<SimpleMason<T>> items = new ArrayList<>();
    @Setter(onMethod_ = {@JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)})
    private Map<String, MasonControl> controls = new LinkedHashMap<>();
}
