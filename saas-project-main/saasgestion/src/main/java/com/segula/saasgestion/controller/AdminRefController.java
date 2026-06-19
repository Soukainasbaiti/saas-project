package com.segula.saasgestion.controller;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.repository.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/ref")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Référentiels Admin", description = "CRUD des données de référence : BU, clients, industries, disciplines, engagements")
@SecurityRequirement(name = "bearerAuth")
public class AdminRefController {

    private final BuRepository                    buRepo;
    private final IndustryRepository              industryRepo;
    private final CustomerRepository              customerRepo;
    private final EngineeringDisciplineRepository disciplineRepo;
    private final EngagementRepository            engagementRepo;
    private final FrontFinancierRepository        frontFinancierRepo;

    // ══════════════════════════════════════════════════════════════
    // BU  (id = String fourni par l'admin)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/bus")
    public ResponseEntity<List<Map<String, Object>>> listBus() {
        return ResponseEntity.ok(buRepo.findAll().stream()
            .sorted(Comparator.comparing(BU::getName))
            .map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",      b.getId());
                m.put("name",    b.getName());
                m.put("trigram", b.getTrigram());
                m.put("bumName", b.getBumName());
                m.put("isActive", b.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/bus")
    public ResponseEntity<?> createBu(@RequestBody Map<String, Object> body) {
        String id = str(body, "id");
        if (id == null || id.isBlank())
            return ResponseEntity.badRequest().body(err("L'identifiant BU est requis"));
        if (buRepo.existsById(id))
            return ResponseEntity.status(409).body(err("Cet ID BU existe déjà"));
        BU bu = BU.builder()
            .id(id.trim().toUpperCase())
            .name(str(body, "name"))
            .trigram(str(body, "trigram"))
            .bumName(str(body, "bumName"))
            .isActive(true)
            .build();
        buRepo.save(bu);
        log.info("BU créée: {}", id);
        return ResponseEntity.status(201).body(ok("BU créée avec succès."));
    }

    @PutMapping("/bus/{id}")
    public ResponseEntity<?> updateBu(@PathVariable String id, @RequestBody Map<String, Object> body) {
        BU bu = buRepo.findById(id).orElse(null);
        if (bu == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name"))    bu.setName(str(body, "name"));
        if (body.containsKey("trigram")) bu.setTrigram(str(body, "trigram"));
        if (body.containsKey("bumName")) bu.setBumName(str(body, "bumName"));
        buRepo.save(bu);
        return ResponseEntity.ok(ok("BU modifiée."));
    }

    @PatchMapping("/bus/{id}/toggle")
    public ResponseEntity<?> toggleBu(@PathVariable String id) {
        BU bu = buRepo.findById(id).orElse(null);
        if (bu == null) return ResponseEntity.notFound().build();
        bu.setActive(!bu.isActive());
        buRepo.save(bu);
        return ResponseEntity.ok(ok(bu.isActive() ? "BU activée." : "BU désactivée."));
    }

    // ══════════════════════════════════════════════════════════════
    // INDUSTRY
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/industries")
    public ResponseEntity<List<Map<String, Object>>> listIndustries() {
        return ResponseEntity.ok(industryRepo.findAll().stream()
            .sorted(Comparator.comparing(Industry::getName))
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",       i.getId());
                m.put("name",     i.getName());
                m.put("trigram",  i.getTrigram());
                m.put("isActive", i.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/industries")
    public ResponseEntity<?> createIndustry(@RequestBody Map<String, Object> body) {
        Industry i = Industry.builder()
            .name(str(body, "name"))
            .trigram(str(body, "trigram").toUpperCase())
            .isActive(true)
            .build();
        try { industryRepo.save(i); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Nom ou trigramme déjà existant."));
        }
        return ResponseEntity.status(201).body(ok("Industry créée."));
    }

    @PutMapping("/industries/{id}")
    public ResponseEntity<?> updateIndustry(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Industry i = industryRepo.findById(id).orElse(null);
        if (i == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name"))    i.setName(str(body, "name"));
        if (body.containsKey("trigram")) i.setTrigram(str(body, "trigram").toUpperCase());
        try { industryRepo.save(i); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Nom ou trigramme déjà existant."));
        }
        return ResponseEntity.ok(ok("Industry modifiée."));
    }

    @PatchMapping("/industries/{id}/toggle")
    public ResponseEntity<?> toggleIndustry(@PathVariable Long id) {
        Industry i = industryRepo.findById(id).orElse(null);
        if (i == null) return ResponseEntity.notFound().build();
        i.setActive(!i.isActive());
        industryRepo.save(i);
        return ResponseEntity.ok(ok(i.isActive() ? "Industry activée." : "Industry désactivée."));
    }

    // ══════════════════════════════════════════════════════════════
    // CUSTOMER
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> listCustomers() {
        return ResponseEntity.ok(customerRepo.findAll().stream()
            .sorted(Comparator.comparing(Customer::getName))
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",            c.getId());
                m.put("name",          c.getName());
                m.put("trigram",       c.getTrigram());
                m.put("customerGroup", c.getCustomerGroup());
                m.put("isActive",      c.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, Object> body) {
        Customer c = Customer.builder()
            .name(str(body, "name"))
            .trigram(str(body, "trigram").toUpperCase())
            .customerGroup(str(body, "customerGroup"))
            .isActive(true)
            .build();
        try { customerRepo.save(c); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Nom ou trigramme déjà existant."));
        }
        return ResponseEntity.status(201).body(ok("Client créé."));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Customer c = customerRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name"))          c.setName(str(body, "name"));
        if (body.containsKey("trigram"))       c.setTrigram(str(body, "trigram").toUpperCase());
        if (body.containsKey("customerGroup")) c.setCustomerGroup(str(body, "customerGroup"));
        try { customerRepo.save(c); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Nom ou trigramme déjà existant."));
        }
        return ResponseEntity.ok(ok("Client modifié."));
    }

    @PatchMapping("/customers/{id}/toggle")
    public ResponseEntity<?> toggleCustomer(@PathVariable Long id) {
        Customer c = customerRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setActive(!c.isActive());
        customerRepo.save(c);
        return ResponseEntity.ok(ok(c.isActive() ? "Client activé." : "Client désactivé."));
    }

    // ══════════════════════════════════════════════════════════════
    // ENGINEERING DISCIPLINE
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/disciplines")
    public ResponseEntity<List<Map<String, Object>>> listDisciplines() {
        return ResponseEntity.ok(disciplineRepo.findAll().stream()
            .sorted(Comparator.comparing(EngineeringDiscipline::getName))
            .map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",       d.getId());
                m.put("name",     d.getName());
                m.put("isActive", d.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/disciplines")
    public ResponseEntity<?> createDiscipline(@RequestBody Map<String, Object> body) {
        EngineeringDiscipline d = EngineeringDiscipline.builder()
            .name(str(body, "name"))
            .isActive(true)
            .build();
        try { disciplineRepo.save(d); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Ce nom existe déjà."));
        }
        return ResponseEntity.status(201).body(ok("Discipline créée."));
    }

    @PutMapping("/disciplines/{id}")
    public ResponseEntity<?> updateDiscipline(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EngineeringDiscipline d = disciplineRepo.findById(id).orElse(null);
        if (d == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name")) d.setName(str(body, "name"));
        try { disciplineRepo.save(d); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Ce nom existe déjà."));
        }
        return ResponseEntity.ok(ok("Discipline modifiée."));
    }

    @PatchMapping("/disciplines/{id}/toggle")
    public ResponseEntity<?> toggleDiscipline(@PathVariable Long id) {
        EngineeringDiscipline d = disciplineRepo.findById(id).orElse(null);
        if (d == null) return ResponseEntity.notFound().build();
        d.setActive(!d.isActive());
        disciplineRepo.save(d);
        return ResponseEntity.ok(ok(d.isActive() ? "Discipline activée." : "Discipline désactivée."));
    }

    // ══════════════════════════════════════════════════════════════
    // ENGAGEMENT
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/engagements")
    public ResponseEntity<List<Map<String, Object>>> listEngagements() {
        return ResponseEntity.ok(engagementRepo.findAll().stream()
            .sorted(Comparator.comparing(Engagement::getName))
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",             e.getId());
                m.put("name",           e.getName());
                m.put("engagementType", e.getEngagementType());
                m.put("isActive",       e.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/engagements")
    public ResponseEntity<?> createEngagement(@RequestBody Map<String, Object> body) {
        Engagement e = Engagement.builder()
            .name(str(body, "name"))
            .engagementType(str(body, "engagementType").toUpperCase())
            .isActive(true)
            .build();
        try { engagementRepo.save(e); } catch (Exception ex) {
            return ResponseEntity.status(409).body(err("Nom ou type déjà existant."));
        }
        return ResponseEntity.status(201).body(ok("Engagement créé."));
    }

    @PutMapping("/engagements/{id}")
    public ResponseEntity<?> updateEngagement(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Engagement e = engagementRepo.findById(id).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name"))           e.setName(str(body, "name"));
        if (body.containsKey("engagementType")) e.setEngagementType(str(body, "engagementType").toUpperCase());
        try { engagementRepo.save(e); } catch (Exception ex) {
            return ResponseEntity.status(409).body(err("Nom ou type déjà existant."));
        }
        return ResponseEntity.ok(ok("Engagement modifié."));
    }

    @PatchMapping("/engagements/{id}/toggle")
    public ResponseEntity<?> toggleEngagement(@PathVariable Long id) {
        Engagement e = engagementRepo.findById(id).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        e.setActive(!e.isActive());
        engagementRepo.save(e);
        return ResponseEntity.ok(ok(e.isActive() ? "Engagement activé." : "Engagement désactivé."));
    }

    // ══════════════════════════════════════════════════════════════
    // FRONT FINANCIER
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/front-financiers")
    public ResponseEntity<List<Map<String, Object>>> listFrontFinanciers() {
        return ResponseEntity.ok(frontFinancierRepo.findAll().stream()
            .sorted(Comparator.comparing(FrontFinancier::getCode))
            .map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",       f.getId());
                m.put("code",     f.getCode());
                m.put("label",    f.getLabel());
                m.put("isActive", f.isActive());
                return m;
            }).toList());
    }

    @PostMapping("/front-financiers")
    public ResponseEntity<?> createFrontFinancier(@RequestBody Map<String, Object> body) {
        FrontFinancier f = FrontFinancier.builder()
            .code(str(body, "code").toUpperCase())
            .label(str(body, "label"))
            .isActive(true)
            .build();
        try { frontFinancierRepo.save(f); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Ce code existe déjà."));
        }
        return ResponseEntity.status(201).body(ok("Front Financier créé."));
    }

    @PutMapping("/front-financiers/{id}")
    public ResponseEntity<?> updateFrontFinancier(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        FrontFinancier f = frontFinancierRepo.findById(id).orElse(null);
        if (f == null) return ResponseEntity.notFound().build();
        if (body.containsKey("code"))  f.setCode(str(body, "code").toUpperCase());
        if (body.containsKey("label")) f.setLabel(str(body, "label"));
        try { frontFinancierRepo.save(f); } catch (Exception e) {
            return ResponseEntity.status(409).body(err("Ce code existe déjà."));
        }
        return ResponseEntity.ok(ok("Front Financier modifié."));
    }

    @PatchMapping("/front-financiers/{id}/toggle")
    public ResponseEntity<?> toggleFrontFinancier(@PathVariable Long id) {
        FrontFinancier f = frontFinancierRepo.findById(id).orElse(null);
        if (f == null) return ResponseEntity.notFound().build();
        f.setActive(!f.isActive());
        frontFinancierRepo.save(f);
        return ResponseEntity.ok(ok(f.isActive() ? "Front Financier activé." : "Front Financier désactivé."));
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private Map<String, String> ok(String msg)  { return Map.of("message", msg); }
    private Map<String, String> err(String msg) { return Map.of("error",   msg); }
}
