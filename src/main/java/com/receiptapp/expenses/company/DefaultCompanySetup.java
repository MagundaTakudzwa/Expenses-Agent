package com.receiptapp.expenses.company;

import com.receiptapp.expenses.category.ClaimType;
import com.receiptapp.expenses.category.ExpenseCategory;
import com.receiptapp.expenses.category.ExpenseCategoryRepository;
import com.receiptapp.expenses.fuel.FuelType;
import com.receiptapp.expenses.fuel.FuelTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The starting categories and fuel types every new company gets. Each company
 * can
 * rename, switch off or add to them afterwards. Changing this list only affects
 * companies that sign up later.
 */
@Component
public class DefaultCompanySetup {

    private record DefaultCategory(String name, ClaimType claimType, boolean requiresReceipt) {
    }

    // Based on common field expenses in Zimbabwe. Most-used first, "Other" last.
    private static final List<DefaultCategory> CATEGORIES = List.of(
            new DefaultCategory("Fuel", ClaimType.FUEL, true),
            new DefaultCategory("Tolls (ZINARA)", ClaimType.RECEIPT, true),
            new DefaultCategory("Mileage (own vehicle)", ClaimType.MILEAGE, false),
            new DefaultCategory("Subsistence (per diem)", ClaimType.SUBSISTENCE, false),
            new DefaultCategory("Vehicle repairs and maintenance", ClaimType.RECEIPT, true),
            new DefaultCategory("Accommodation", ClaimType.RECEIPT, true),
            new DefaultCategory("Airtime and data", ClaimType.RECEIPT, true),
            new DefaultCategory("Public transport", ClaimType.RECEIPT, true),
            new DefaultCategory("Tools and equipment", ClaimType.RECEIPT, true),
            new DefaultCategory("Protective clothing (PPE)", ClaimType.RECEIPT, true),
            new DefaultCategory("Field camp and equipment", ClaimType.RECEIPT, true),
            new DefaultCategory("Office supplies", ClaimType.RECEIPT, true),
            new DefaultCategory("Other", ClaimType.RECEIPT, true));

    private static final List<String> FUEL_TYPES = List.of("Diesel", "Petrol (Blend)");

    private final ExpenseCategoryRepository categories;
    private final FuelTypeRepository fuelTypes;

    public DefaultCompanySetup(ExpenseCategoryRepository categories, FuelTypeRepository fuelTypes) {
        this.categories = categories;
        this.fuelTypes = fuelTypes;
    }

    /**
     * Must run inside the sign-up transaction, so a company never exists without
     * its defaults.
     */
    public void createDefaultsFor(Company company) {
        int order = 10;
        for (DefaultCategory c : CATEGORIES) {
            categories.save(new ExpenseCategory(company, c.name(), c.claimType(), c.requiresReceipt(), order));
            order += 10;
        }
        order = 10;
        for (String name : FUEL_TYPES) {
            fuelTypes.save(new FuelType(company, name, order));
            order += 10;
        }
    }
}