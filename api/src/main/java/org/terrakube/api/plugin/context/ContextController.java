package org.terrakube.api.plugin.context;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.terrakube.api.plugin.storage.StorageTypeService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/context/v1")
@AllArgsConstructor
public class ContextController {
    StorageTypeService storageTypeService;

    @GetMapping(value= "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getContext(@PathVariable("jobId") int jobId) throws IOException {
        String context = storageTypeService.getContext(jobId);
        return new ResponseEntity<>(context, HttpStatus.OK);
    }

    @PostMapping(value= "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveContext(@PathVariable("jobId") int jobId, @RequestBody String context) throws IOException {
        String savedContext = "{}";
        try {
            new ObjectMapper().readTree(context);
            savedContext = storageTypeService.saveContext(jobId, context);
        } catch (JacksonException e) {
            log.error(e.getMessage());
        }

        return new ResponseEntity<>(savedContext, HttpStatus.OK);
    }
}
