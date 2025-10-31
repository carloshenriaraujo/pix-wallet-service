
package com.meva.finance.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class Controller {

	@PostMapping("/teste")
	private void teste() throws Exception {
		log.info("OK");

	}

}
