package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingItemSelection;
import RMC_Booking_Engine.rmc.domain.entity.BookingServiceSelection;
import RMC_Booking_Engine.rmc.domain.entity.RoomItemAddon;
import RMC_Booking_Engine.rmc.domain.entity.RoomServiceAddon;
import RMC_Booking_Engine.rmc.dto.AddonDeleteResult;
import RMC_Booking_Engine.rmc.dto.BookingItemAddonSelectionRequest;
import RMC_Booking_Engine.rmc.dto.BookingItemSelectionDto;
import RMC_Booking_Engine.rmc.dto.BookingServiceSelectionDto;
import RMC_Booking_Engine.rmc.dto.CreateItemAddonRequest;
import RMC_Booking_Engine.rmc.dto.CreateServiceAddonRequest;
import RMC_Booking_Engine.rmc.dto.GuestItemAddonDto;
import RMC_Booking_Engine.rmc.dto.GuestServiceAddonDto;
import RMC_Booking_Engine.rmc.dto.ItemAddonDto;
import RMC_Booking_Engine.rmc.dto.ServiceAddonDto;
import RMC_Booking_Engine.rmc.dto.UpdateItemAddonRequest;
import RMC_Booking_Engine.rmc.dto.UpdateServiceAddonRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingItemSelectionRepository;
import RMC_Booking_Engine.rmc.repository.BookingServiceSelectionRepository;
import RMC_Booking_Engine.rmc.repository.RoomItemAddonRepository;
import RMC_Booking_Engine.rmc.repository.RoomServiceAddonRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomExtrasService {

    private final RoomServiceAddonRepository serviceAddonRepository;
    private final RoomItemAddonRepository itemAddonRepository;
    private final BookingServiceSelectionRepository bookingServiceSelectionRepository;
    private final BookingItemSelectionRepository bookingItemSelectionRepository;

    @Transactional(readOnly = true)
    public List<GuestServiceAddonDto> listGuestServices() {
        return serviceAddonRepository.findByActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .map(this::toGuestService)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuestItemAddonDto> listGuestItems() {
        return itemAddonRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toGuestItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceAddonDto> listStaffServices() {
        return serviceAddonRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .map(this::toStaffService)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemAddonDto> listStaffItems() {
        return itemAddonRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toStaffItem)
                .toList();
    }

    @Transactional
    public ServiceAddonDto createService(CreateServiceAddonRequest request) {
        validateAddonPricing(request.free(), request.price(), "service");
        RoomServiceAddon addon = new RoomServiceAddon();
        addon.setTitle(request.title().trim());
        addon.setSubtitle(trimOrNull(request.subtitle()));
        addon.setDetails(trimOrNull(request.details()));
        addon.setImageUrl(trimOrNull(request.imageUrl()));
        addon.setFree(request.free());
        addon.setPrice(request.free() ? null : request.price());
        addon.setActive(request.active());
        addon.setSortOrder(nextServiceSortOrder());
        addon.setCreatedAt(Instant.now());
        return toStaffService(serviceAddonRepository.save(addon));
    }

    @Transactional
    public ServiceAddonDto updateService(Long id, UpdateServiceAddonRequest request) {
        validateAddonPricing(request.free(), request.price(), "service");
        RoomServiceAddon addon = serviceAddonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service add-on not found"));
        addon.setTitle(request.title().trim());
        addon.setSubtitle(trimOrNull(request.subtitle()));
        addon.setDetails(trimOrNull(request.details()));
        addon.setImageUrl(trimOrNull(request.imageUrl()));
        addon.setFree(request.free());
        addon.setPrice(request.free() ? null : request.price());
        addon.setActive(request.active());
        addon.setSortOrder(request.sortOrder());
        addon.setUpdatedAt(Instant.now());
        return toStaffService(serviceAddonRepository.save(addon));
    }

    @Transactional
    public AddonDeleteResult deleteService(Long id) {
        RoomServiceAddon addon = serviceAddonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service add-on not found"));
        if (bookingServiceSelectionRepository.existsByServiceAddonId(id)) {
            addon.setActive(false);
            addon.setUpdatedAt(Instant.now());
            serviceAddonRepository.save(addon);
            return new AddonDeleteResult(
                    true,
                    "Service add-on is used by existing bookings, so it was deactivated instead of deleted");
        }
        serviceAddonRepository.delete(addon);
        return new AddonDeleteResult(false, "Service add-on deleted");
    }

    @Transactional
    public ItemAddonDto createItem(CreateItemAddonRequest request) {
        validateAddonPricing(request.free(), request.price(), "item");
        RoomItemAddon addon = new RoomItemAddon();
        addon.setName(request.name().trim());
        addon.setSubtitle(trimOrNull(request.subtitle()));
        addon.setDetails(trimOrNull(request.details()));
        addon.setImageUrl(trimOrNull(request.imageUrl()));
        addon.setFree(request.free());
        addon.setPrice(request.free() ? null : request.price());
        addon.setActive(request.active());
        addon.setSortOrder(nextItemSortOrder());
        addon.setCreatedAt(Instant.now());
        return toStaffItem(itemAddonRepository.save(addon));
    }

    @Transactional
    public ItemAddonDto updateItem(Long id, UpdateItemAddonRequest request) {
        validateAddonPricing(request.free(), request.price(), "item");
        RoomItemAddon addon = itemAddonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Item add-on not found"));
        addon.setName(request.name().trim());
        addon.setSubtitle(trimOrNull(request.subtitle()));
        addon.setDetails(trimOrNull(request.details()));
        addon.setImageUrl(trimOrNull(request.imageUrl()));
        addon.setFree(request.free());
        addon.setPrice(request.free() ? null : request.price());
        addon.setActive(request.active());
        addon.setSortOrder(request.sortOrder());
        addon.setUpdatedAt(Instant.now());
        return toStaffItem(itemAddonRepository.save(addon));
    }

    @Transactional
    public AddonDeleteResult deleteItem(Long id) {
        RoomItemAddon addon = itemAddonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Item add-on not found"));
        if (bookingItemSelectionRepository.existsByItemAddonId(id)) {
            addon.setActive(false);
            addon.setUpdatedAt(Instant.now());
            itemAddonRepository.save(addon);
            return new AddonDeleteResult(
                    true,
                    "Item add-on is used by existing bookings, so it was deactivated instead of deleted");
        }
        itemAddonRepository.delete(addon);
        return new AddonDeleteResult(false, "Item add-on deleted");
    }

    @Transactional
    public BigDecimal applyBookingExtras(
            Booking booking,
            List<Long> serviceAddonIds,
            List<BookingItemAddonSelectionRequest> itemSelections,
            String customExtrasRequest) {
        booking.setCustomExtrasRequest(trimOrNull(customExtrasRequest));
        BigDecimal servicesTotal = persistServiceSelections(booking, serviceAddonIds);
        BigDecimal itemsTotal = persistItemSelections(booking, itemSelections);
        return servicesTotal.add(itemsTotal);
    }

    @Transactional(readOnly = true)
    public List<BookingServiceSelectionDto> getServiceSelectionsForBooking(Long bookingId) {
        return bookingServiceSelectionRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .map(selection -> new BookingServiceSelectionDto(
                        selection.getTitle(), selection.getLineTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingItemSelectionDto> getItemSelectionsForBooking(Long bookingId) {
        return bookingItemSelectionRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .filter(BookingItemSelection::isSelected)
                .map(selection -> new BookingItemSelectionDto(
                        selection.getItemName(),
                        selection.isSelected(),
                        selection.getGuestNote(),
                        selection.getUnitPrice()))
                .toList();
    }

    private BigDecimal persistServiceSelections(Booking booking, List<Long> serviceAddonIds) {
        if (serviceAddonIds == null || serviceAddonIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (Long id : serviceAddonIds) {
            if (id == null || !uniqueIds.add(id)) {
                throw new BusinessException("Duplicate or invalid service add-on");
            }
        }

        Map<Long, RoomServiceAddon> addons = serviceAddonRepository.findAllById(uniqueIds).stream()
                .filter(RoomServiceAddon::isActive)
                .collect(Collectors.toMap(RoomServiceAddon::getId, Function.identity()));

        if (addons.size() != uniqueIds.size()) {
            throw new BusinessException("One or more service add-ons are unavailable");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Long id : serviceAddonIds) {
            RoomServiceAddon addon = addons.get(id);
            BigDecimal lineTotal = addon.isFree() || addon.getPrice() == null
                    ? BigDecimal.ZERO
                    : addon.getPrice();

            BookingServiceSelection selection = new BookingServiceSelection();
            selection.setBooking(booking);
            selection.setServiceAddon(addon);
            selection.setTitle(addon.getTitle());
            selection.setUnitPrice(lineTotal);
            selection.setLineTotal(lineTotal);
            bookingServiceSelectionRepository.save(selection);
            total = total.add(lineTotal);
        }
        return total;
    }

    private BigDecimal persistItemSelections(
            Booking booking, List<BookingItemAddonSelectionRequest> itemSelections) {
        if (itemSelections == null || itemSelections.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> seen = new HashSet<>();
        List<BookingItemAddonSelectionRequest> selected = new ArrayList<>();
        for (BookingItemAddonSelectionRequest selection : itemSelections) {
            if (selection == null || selection.itemId() == null || !selection.selected()) {
                continue;
            }
            if (!seen.add(selection.itemId())) {
                throw new BusinessException("Duplicate item add-on selection");
            }
            selected.add(selection);
        }

        if (selected.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> ids = selected.stream()
                .map(BookingItemAddonSelectionRequest::itemId)
                .collect(Collectors.toSet());
        Map<Long, RoomItemAddon> addons = itemAddonRepository.findAllById(ids).stream()
                .filter(RoomItemAddon::isActive)
                .collect(Collectors.toMap(RoomItemAddon::getId, Function.identity()));

        if (addons.size() != ids.size()) {
            throw new BusinessException("One or more item add-ons are unavailable");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BookingItemAddonSelectionRequest request : selected) {
            RoomItemAddon addon = addons.get(request.itemId());
            BigDecimal lineTotal = addon.isFree() || addon.getPrice() == null
                    ? BigDecimal.ZERO
                    : addon.getPrice();

            BookingItemSelection selection = new BookingItemSelection();
            selection.setBooking(booking);
            selection.setItemAddon(addon);
            selection.setItemName(addon.getName());
            selection.setSelected(true);
            selection.setGuestNote(null);
            selection.setUnitPrice(lineTotal);
            bookingItemSelectionRepository.save(selection);
            total = total.add(lineTotal);
        }
        return total;
    }

    private void validateAddonPricing(boolean free, BigDecimal price, String kind) {
        if (!free && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("Paid " + kind + " add-ons require a price greater than zero");
        }
    }

    private int nextServiceSortOrder() {
        return serviceAddonRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .mapToInt(RoomServiceAddon::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private int nextItemSortOrder() {
        return itemAddonRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .mapToInt(RoomItemAddon::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private GuestServiceAddonDto toGuestService(RoomServiceAddon addon) {
        return new GuestServiceAddonDto(
                addon.getId(),
                addon.getTitle(),
                addon.getSubtitle(),
                addon.getDetails(),
                addon.getImageUrl(),
                addon.isFree(),
                addon.isFree() ? null : addon.getPrice());
    }

    private GuestItemAddonDto toGuestItem(RoomItemAddon addon) {
        return new GuestItemAddonDto(
                addon.getId(),
                addon.getName(),
                addon.getSubtitle(),
                addon.getDetails(),
                addon.getImageUrl(),
                addon.isFree(),
                addon.isFree() ? null : addon.getPrice());
    }

    private ServiceAddonDto toStaffService(RoomServiceAddon addon) {
        return new ServiceAddonDto(
                addon.getId(),
                addon.getTitle(),
                addon.getSubtitle(),
                addon.getDetails(),
                addon.getImageUrl(),
                addon.isFree(),
                addon.getPrice(),
                addon.isActive(),
                addon.getSortOrder());
    }

    private ItemAddonDto toStaffItem(RoomItemAddon addon) {
        return new ItemAddonDto(
                addon.getId(),
                addon.getName(),
                addon.getSubtitle(),
                addon.getDetails(),
                addon.getImageUrl(),
                addon.isFree(),
                addon.getPrice(),
                addon.isActive(),
                addon.getSortOrder());
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
