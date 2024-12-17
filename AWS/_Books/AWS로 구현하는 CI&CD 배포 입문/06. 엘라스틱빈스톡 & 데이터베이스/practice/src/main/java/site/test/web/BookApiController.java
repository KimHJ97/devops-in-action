package site.test.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.test.service.BookService;
import site.test.web.dto.BookRespDto;
import site.test.web.dto.BookSaveReqDto;

@RequiredArgsConstructor
@RestController
public class BookApiController {

    private final BookService bookService;

    @GetMapping("/")
    public String home() {
        return "<h1>aws-v4</h1>";
    }

    @PostMapping("/api/book")
    public ResponseEntity<?> bookSave(@RequestBody BookSaveReqDto reqDto) {
        BookRespDto respDto = bookService.책등록(reqDto);
        return new ResponseEntity<>(respDto, HttpStatus.CREATED);
    }

    @GetMapping("/api/book")
    public ResponseEntity<?> bookList() {
        List<BookRespDto> respDtos = bookService.책목록보기();
        return new ResponseEntity<>(respDtos, HttpStatus.OK);
    }
}
