package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.CreatePromoCodeRequest;
import RMC_Booking_Engine.rmc.dto.PromoCodeDto;
import RMC_Booking_Engine.rmc.dto.UpdatePromoCodeRequest;
import RMC_Booking_Engine.rmc.service.PromoCodeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/promo-codes")
@RequiredArgsConstructor
public class StaffPromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping
    public List<PromoCodeDto> list() {
        return promoCodeService.listStaffPromoCodes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeDto create(@Valid @RequestBody CreatePromoCodeRequest request) {
        return promoCodeService.create(request);
    }

    @PutMapping("/{id}")
    public PromoCodeDto update(@PathVariable Long id, @Valid @RequestBody UpdatePromoCodeRequest request) {
        return promoCodeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        promoCodeService.delete(id);
    }
}
