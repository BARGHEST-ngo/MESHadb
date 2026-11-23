// Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later

package session

import (
	"bytes"
	"strings"
	"testing"

	meshnet "github.com/BARGHEST-ngo/MESHnet"
)

func TestSanitizeName(t *testing.T) {
	tests := []struct{ name, in, want string }{
		{"plain", "Pixel 6a", "Pixel 6a"},
		{"non-ASCII kept", "Téléphone 📱", "Téléphone 📱"},
		{"CSI clear screen", "a\x1b[2Jb", "a[2Jb"},
		{"OSC title with BEL", "\x1b]0;pwned\x07x", "]0;pwnedx"},
		{"C1 CSI", "a\u009b2Jb", "a2Jb"},
		{"carriage return and newline", "ok\r\nmeshcli> ", "okmeshcli>"},
		{"bidi override", "a‮gnp.exe", "agnp.exe"},
		{"invalid UTF-8", "a\xffb", "ab"},
		{"only control bytes", "\x1b\x07\r\n", "unnamed device"},
		{"empty", "", "unnamed device"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := sanitizeName(tt.in); got != tt.want {
				t.Errorf("sanitizeName(%q) = %q, want %q", tt.in, got, tt.want)
			}
		})
	}
}

// A hostile name must not reach the terminal raw on connect or disconnect, nor be stored raw
func TestDeviceNameSanitizedEverywhere(t *testing.T) {
	var out bytes.Buffer
	s := &Session{devices: make(map[string]*device), out: &out}
	hostile := meshnet.ClientInfo{ID: "id", Name: "evil\x1b[2J\r\nmeshcli> "}

	s.connected(hostile)
	if got := s.ClientName("id"); got != "evil[2Jmeshcli>" {
		t.Errorf("stored name = %q", got)
	}
	if got := s.Clients()[0].Name; got != "evil[2Jmeshcli>" {
		t.Errorf("Clients() name = %q", got)
	}

	s.disconnected(hostile, nil)

	if strings.ContainsAny(out.String(), "\x1b\r") {
		t.Errorf("control bytes reached the terminal: %q", out.String())
	}
	if want := "device connected: evil[2Jmeshcli>\ndevice disconnected: evil[2Jmeshcli>\n"; out.String() != want {
		t.Errorf("output = %q, want %q", out.String(), want)
	}
}
