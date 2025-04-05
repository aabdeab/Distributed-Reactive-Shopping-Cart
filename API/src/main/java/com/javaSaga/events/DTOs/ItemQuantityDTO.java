package com.javaSaga.events.DTOs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemQuantityDTO {
    private int ReservedQuantity;
    private int AvailableQuantity;
    private Date LastModified;
}
