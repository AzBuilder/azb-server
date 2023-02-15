package org.terrakube.api.plugin.scheduler.job.tcl.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
@Setter
public class Flow {
    private String type;
    private String team;
    private String name;

    private String error;
    private int step;
    List<Command> commands;

    ImportComands importComands;
}
