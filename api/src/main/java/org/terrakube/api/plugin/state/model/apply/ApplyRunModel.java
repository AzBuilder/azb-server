package org.terrakube.api.plugin.state.model.apply;


import lombok.Setter;
import lombok.Getter;
import org.terrakube.api.plugin.state.model.generic.Resource;

import java.util.Map;

@Getter
@Setter
public class ApplyRunModel extends Resource {
    Map<String, Object> attributes;
}
