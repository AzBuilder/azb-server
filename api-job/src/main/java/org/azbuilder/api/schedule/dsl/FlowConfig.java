package org.azbuilder.api.schedule.dsl;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;

@ToString
@Getter
@Setter
public class FlowConfig {
    LinkedList<Flow> flow;
}