package com.myapp.investment_dashboard_backend.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePortfolioRequest {

    @NotBlank(message = "Portfolio name cannot be blank")
    @Size(max = 100, message = "Portfolio name cannot exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

}