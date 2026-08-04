package com.showroom.orchestrator.graphql;

import com.showroom.orchestrator.client.VehicleServiceClient;
import com.showroom.orchestrator.dto.CreateConfigurationInput;
import com.showroom.orchestrator.dto.CreateConfigurationRequest;
import com.showroom.orchestrator.dto.ModelFilter;
import com.showroom.orchestrator.dto.VehicleConfiguration;
import com.showroom.orchestrator.dto.VehicleModel;
import com.showroom.orchestrator.error.InvalidInputException;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Thin GraphQL controller. Each data fetcher delegates synchronously to the
 * downstream Vehicle Config service and maps the result straight to the
 * GraphQL types; no business logic lives here. The blocking client runs on a
 * servlet thread, so this service uses the plain servlet (MVC) stack.
 */
@Controller
public class ShowroomGraphQlController {

    private final VehicleServiceClient client;

    public ShowroomGraphQlController(VehicleServiceClient client) {
        this.client = client;
    }

    @QueryMapping
    public List<VehicleModel> models(@Argument ModelFilter filter) {
        return client.getModels(filter);
    }

    @QueryMapping
    public VehicleModel model(@Argument String id) {
        return client.getModel(id);
    }

    @QueryMapping
    public VehicleConfiguration configuration(@Argument String id) {
        return client.getConfiguration(id);
    }

    @MutationMapping
    public VehicleConfiguration createConfiguration(@Argument("input") CreateConfigurationInput input) {
        if (input == null || isBlank(input.modelId()) || isBlank(input.trimId())) {
            throw new InvalidInputException("modelId and trimId are required");
        }
        List<String> optionIds = input.optionIds() == null ? List.of() : input.optionIds();
        return client.createConfiguration(new CreateConfigurationRequest(input.modelId(), input.trimId(), optionIds));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
