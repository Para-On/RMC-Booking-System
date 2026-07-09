UPDATE staff_nav_module child
JOIN staff_nav_module parent ON parent.module_key = 'settings' AND parent.parent_id IS NULL
SET child.parent_id = parent.id,
    child.sort_order = 15
WHERE child.module_key = 'branding'
  AND child.parent_id IS NULL;
