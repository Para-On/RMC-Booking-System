package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffNavModule;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.CreateStaffNavModuleRequest;
import RMC_Booking_Engine.rmc.dto.StaffNavModuleDto;
import RMC_Booking_Engine.rmc.dto.StaffRoleOptionDto;
import RMC_Booking_Engine.rmc.dto.UpdateStaffNavModuleRequest;
import RMC_Booking_Engine.rmc.dto.UpdateStaffNavModulesRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.StaffNavModuleRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.JsonStringListConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffNavService {

    private static final String GROUP_PATH = "#";

    private final StaffNavModuleRepository staffNavModuleRepository;

    @Transactional(readOnly = true)
    public List<StaffNavModuleDto> getNavForStaff(StaffPrincipal staff) {
        List<StaffNavModule> all = staffNavModuleRepository.findAllByOrderBySortOrderAsc();
        return buildTree(all, true, staff.role().name());
    }

    @Transactional(readOnly = true)
    public List<StaffNavModuleDto> getAllModules() {
        List<StaffNavModule> all = staffNavModuleRepository.findAllByOrderBySortOrderAsc();
        return buildTree(all, false, null);
    }

    @Transactional(readOnly = true)
    public List<StaffRoleOptionDto> getRoleOptions() {
        return Arrays.stream(StaffRole.values())
                .map(role -> new StaffRoleOptionDto(role.name(), formatRoleLabel(role)))
                .toList();
    }

    @Transactional
    public StaffNavModuleDto createModule(CreateStaffNavModuleRequest request) {
        String moduleKey = normalizeKey(request.moduleKey());
        if (staffNavModuleRepository.existsByModuleKey(moduleKey)) {
            throw new BusinessException("Module key already exists: " + moduleKey);
        }

        StaffNavModule parent = resolveParent(request.parentId(), null);
        validatePath(request.path(), parent != null);
        List<String> allowedRoles = validateRoles(request.allowedRoles());

        StaffNavModule module = new StaffNavModule();
        module.setModuleKey(moduleKey);
        module.setLabel(request.label().trim());
        module.setPath(normalizePath(request.path()));
        module.setIcon(request.icon().trim());
        module.setAllowedRoles(JsonStringListConverter.toJson(allowedRoles));
        module.setParent(parent);
        module.setSortOrder(resolveSortOrder(request.sortOrder(), request.parentId()));
        module.setEnabled(request.enabled() == null || request.enabled());

        StaffNavModule saved = staffNavModuleRepository.save(module);
        return toDtoWithChildren(saved, staffNavModuleRepository.findAllByOrderBySortOrderAsc(), false, null);
    }

    @Transactional
    public StaffNavModuleDto updateModule(Long id, UpdateStaffNavModuleRequest request) {
        StaffNavModule module = staffNavModuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Nav module not found: " + id));

        boolean isSubModule = module.getParent() != null;
        validatePath(request.path(), isSubModule);
        List<String> allowedRoles = validateRoles(request.allowedRoles());

        module.setLabel(request.label().trim());
        module.setPath(normalizePath(request.path()));
        module.setIcon(request.icon().trim());
        module.setAllowedRoles(JsonStringListConverter.toJson(allowedRoles));
        if (request.enabled() != null) {
            module.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            module.setSortOrder(request.sortOrder());
        }

        StaffNavModule saved = staffNavModuleRepository.save(module);
        return toDtoWithChildren(saved, staffNavModuleRepository.findAllByOrderBySortOrderAsc(), false, null);
    }

    @Transactional
    public void deleteModule(Long id) {
        StaffNavModule module = staffNavModuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Nav module not found: " + id));
        staffNavModuleRepository.delete(module);
    }

    @Transactional
    public List<StaffNavModuleDto> updateModules(UpdateStaffNavModulesRequest request) {
        for (UpdateStaffNavModulesRequest.StaffNavModuleUpdateItem item : request.modules()) {
            StaffNavModule module = staffNavModuleRepository.findById(item.id())
                    .orElseThrow(() -> new BusinessException("Nav module not found: " + item.id()));
            if (item.enabled() != null) {
                module.setEnabled(item.enabled());
            }
            if (item.sortOrder() != null) {
                module.setSortOrder(item.sortOrder());
            }
            staffNavModuleRepository.save(module);
        }
        return getAllModules();
    }

    private List<StaffNavModuleDto> buildTree(List<StaffNavModule> all, boolean filterForStaff, String staffRole) {
        List<StaffNavModule> roots = all.stream()
                .filter(module -> module.getParent() == null)
                .sorted(Comparator.comparing(StaffNavModule::getSortOrder))
                .toList();

        List<StaffNavModuleDto> tree = new ArrayList<>();
        for (StaffNavModule root : roots) {
            StaffNavModuleDto dto = toDtoWithChildren(root, all, filterForStaff, staffRole);
            if (dto != null) {
                tree.add(dto);
            }
        }
        return tree;
    }

    private StaffNavModuleDto toDtoWithChildren(
            StaffNavModule module, List<StaffNavModule> all, boolean filterForStaff, String staffRole) {
        if (filterForStaff && !Boolean.TRUE.equals(module.getEnabled())) {
            return null;
        }

        List<StaffNavModuleDto> children = all.stream()
                .filter(item -> item.getParent() != null && Objects.equals(item.getParent().getId(), module.getId()))
                .sorted(Comparator.comparing(StaffNavModule::getSortOrder))
                .map(child -> toDtoWithChildren(child, all, filterForStaff, staffRole))
                .filter(Objects::nonNull)
                .toList();

        boolean roleAllowed = !filterForStaff || canAccess(staffRole, module.getAllowedRoles());
        if (filterForStaff && !roleAllowed && children.isEmpty()) {
            return null;
        }

        return new StaffNavModuleDto(
                module.getId(),
                module.getParent() != null ? module.getParent().getId() : null,
                module.getModuleKey(),
                module.getLabel(),
                module.getPath(),
                module.getIcon(),
                JsonStringListConverter.fromJson(module.getAllowedRoles()),
                module.getSortOrder(),
                Boolean.TRUE.equals(module.getEnabled()),
                children);
    }

    private StaffNavModule resolveParent(Long parentId, Long currentModuleId) {
        if (parentId == null) {
            return null;
        }
        if (Objects.equals(parentId, currentModuleId)) {
            throw new BusinessException("A module cannot be its own parent");
        }
        StaffNavModule parent = staffNavModuleRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException("Parent module not found: " + parentId));
        if (parent.getParent() != null) {
            throw new BusinessException("Sub-modules can only be added under top-level modules");
        }
        return parent;
    }

    private void validatePath(String path, boolean isSubModule) {
        String normalized = normalizePath(path);
        if (GROUP_PATH.equals(normalized)) {
            if (isSubModule) {
                throw new BusinessException("Sub-modules must link to a staff page");
            }
            return;
        }
        if (!normalized.startsWith("/staff/")) {
            throw new BusinessException("Path must start with /staff/");
        }
    }

    private List<String> validateRoles(List<String> roles) {
        List<String> cleaned = JsonStringListConverter.sanitize(roles);
        if (cleaned.isEmpty()) {
            throw new BusinessException("At least one role must be selected");
        }
        for (String role : cleaned) {
            try {
                StaffRole.valueOf(role);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Unknown staff role: " + role);
            }
        }
        return cleaned;
    }

    private int resolveSortOrder(Integer requested, Long parentId) {
        if (requested != null) {
            return requested;
        }
        return staffNavModuleRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(module -> {
                    if (parentId == null) {
                        return module.getParent() == null;
                    }
                    return module.getParent() != null && Objects.equals(module.getParent().getId(), parentId);
                })
                .mapToInt(StaffNavModule::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private boolean canAccess(String staffRole, String allowedRolesJson) {
        if (StaffRole.ADMIN.name().equals(staffRole)) {
            return true;
        }
        return JsonStringListConverter.fromJson(allowedRolesJson).contains(staffRole);
    }

    private String normalizeKey(String moduleKey) {
        return moduleKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
    }

    private String normalizePath(String path) {
        return path.trim();
    }

    private String formatRoleLabel(StaffRole role) {
        return switch (role) {
            case FRONT_DESK -> "Front desk";
            case MANAGER -> "Manager";
            case ADMIN -> "Admin";
        };
    }
}
