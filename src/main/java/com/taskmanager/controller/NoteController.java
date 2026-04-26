package com.taskmanager.controller;

import com.taskmanager.entity.Note;
import com.taskmanager.repository.NoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class NoteController {

    private final NoteRepository noteRepository;

    public NoteController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping("/notes")
    public String notesPage(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String sort,
                            @RequestParam(required = false) Long editId,
                            Model model) {

        boolean isSearch = keyword != null && !keyword.trim().isEmpty();

        List<Note> notes = isSearch
                ? noteRepository.findByTitleContainingIgnoreCase(keyword)
                : noteRepository.findAllByOrderByPositionAsc();

        if (sort != null) {
            switch (sort) {
                case "az" -> notes.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                case "newest" -> notes.sort((a, b) -> b.getId().compareTo(a.getId()));
                case "oldest" -> notes.sort((a, b) -> a.getId().compareTo(b.getId()));
            }
        }

        Note note = new Note();
        boolean editMode = false;

        if (editId != null) {
            note = noteRepository.findById(editId)
                    .orElseThrow(() -> new RuntimeException("Note not found"));
            editMode = true;
        }

        List<Note> activeNotes = notes.stream()
                .filter(n -> !n.isCompleted())
                .collect(Collectors.toList());

        List<Note> completedNotes = notes.stream()
                .filter(Note::isCompleted)
                .collect(Collectors.toList());

        model.addAttribute("notes", activeNotes);
        model.addAttribute("completedNotes", completedNotes);
        model.addAttribute("completedCount", completedNotes.size());
        model.addAttribute("note", note);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("editMode", editMode);

        return "notes";
    }

    @PostMapping("/notes")
    public String saveNote(@ModelAttribute Note note) {
        noteRepository.save(note);
        return "redirect:/notes";
    }

    @PostMapping("/notes/update/{id}")
    public String updateNote(@PathVariable Long id, @ModelAttribute Note note) {
        Note existing = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        existing.setTitle(note.getTitle());
        existing.setContent(note.getContent());

        noteRepository.save(existing);

        return "redirect:/notes";
    }

    @GetMapping("/notes/delete/{id}")
    public String delete(@PathVariable Long id) {
        noteRepository.deleteById(id);
        return "redirect:/notes";
    }

    @PostMapping("/notes/toggle/{id}")
    public String toggleNoteCompleted(@PathVariable Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        note.setCompleted(!note.isCompleted());
        noteRepository.save(note);

        return "redirect:/notes";
    }

    @PostMapping("/notes/reorder")
    @ResponseBody
    public void reorderNotes(@RequestBody List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Note note = noteRepository.findById(ids.get(i))
                    .orElseThrow(() -> new RuntimeException("Note not found"));

            note.setPosition(i);
            noteRepository.save(note);
        }
    }
}
