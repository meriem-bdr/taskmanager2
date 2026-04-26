package com.taskmanager.service;

import com.taskmanager.entity.Note;
import com.taskmanager.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository repo;

    public NoteService(NoteRepository repo) {
        this.repo = repo;
    }

    // ✅ GET ALL (with order)
    public List<Note> getAllNotes() {
        return repo.findAllByOrderByPositionAsc();
    }

    // ✅ SAVE / UPDATE
    public void save(Note note) {
        repo.save(note);
    }

    // ✅ DELETE
    public void delete(Long id) {
        repo.deleteById(id);
    }

    // ✅ SEARCH
    public List<Note> searchNotes(String keyword) {
        return repo.findByTitleContainingIgnoreCase(keyword);
    }

    // ✅ REORDER (drag & drop)
    public void reorder(List<Long> ids) {
        int index = 0;

        for (Long id : ids) {
            Note note = repo.findById(id).orElse(null);

            if (note != null) {
                note.setPosition(index++);
                repo.save(note);
            }
        }
    }
}
